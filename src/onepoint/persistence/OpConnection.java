/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

/**
 * Super class for all connection implementations.
 */
public abstract class OpConnection {

   public static final int FLUSH_MODE_MANUAL = 0;
   public static final int FLUSH_MODE_COMMIT = 1;
   public static final int FLUSH_MODE_AUTO = 2;
   public static final int FLUSH_MODE_ALWAYS = 3;

   /**
    * The source the connection belongs to
    */
   private OpSource source;

   /**
    * Creates a new connection that will use provided data source.
    *
    * @param source data source.
    */
   public OpConnection(OpSource source) {
      this.source = source;
   }

   /**
    * Retrieve data source used by this connection.
    *
    * @return data source
    */
   public final OpSource getSource() {
      return source;
   }

   public abstract void createSchema();

//   public abstract void updateSchema();

   public abstract void dropSchema();

   public abstract void updateDBSchema()
   		throws SQLException;

   public abstract void persistObject(OpObjectIfc opObject);

   public abstract <C extends OpObjectIfc> C getObject(Class<C> c, long id);

   public abstract void updateObject(OpObjectIfc object);

   public abstract void deleteObject(OpObjectIfc object);

   public abstract void refreshObject(OpObjectIfc object);

   public abstract List list(OpQuery query);

   public abstract Iterator iterate(OpQuery query);

   public abstract int execute(OpQuery query);

   public abstract void flush();

   public abstract OpTransaction newTransaction();

   public abstract Blob newBlob(byte[] bytes);

   public abstract OpQuery newQuery(String s);

   public abstract void close(); // On order to free resources (e.g., JDBC-driver)

   public abstract Connection getJDBCConnection();

   public abstract boolean isValid();

   public abstract boolean isOpen();

   public abstract void setFlushMode(int flushMode);

   public abstract int getFlushMode();

   public abstract void clear();
   
}
