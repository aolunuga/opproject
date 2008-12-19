/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence.sql;

import java.util.HashMap;
import java.util.Map;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;

/**
 * Factory class that produces different sql statements, based on the type of the db.
 *
 * @author horia.chiorean
 */
public final class OpSqlStatementFactory {

   /**
    * Constants for different db types.
    */
   public static final int MYSQL = 1;
   public static final int POSTGRESQL = 2;
   public static final int MSSQL = 3;
   public static final int ORACLE = 4;
   public static final int DB2 = 5;
   public static final int DERBY = 6;

   /**
    * A cache for the statements.
    */
   private static Map statementCache = new HashMap();

   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpSqlStatementFactory.class);

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
            case MSSQL: {
               statement = new OpMSSqlStatement();
               break;
            }
            case ORACLE: {
               statement = new OpOracleStatement();
               break;
            }
            case DB2: {
               statement = new OpDB2Statement();
               break;
            }
            case DERBY: {
               statement = new OpDerbyStatement();
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
