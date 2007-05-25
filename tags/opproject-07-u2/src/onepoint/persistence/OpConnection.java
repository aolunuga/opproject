/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence;

import java.sql.Blob;
import java.sql.Connection;
import java.util.Iterator;
import java.util.List;

public abstract class OpConnection { // Extra interfaces XReadConnection/XWriteConnection?

   private OpSource source; // The source the connection belongs to

   public OpConnection(OpSource source) {
      this.source = source;
   }

   public final OpSource getSource() {
      return source;
   }

   // public abstract void createPrototype(String name);

   // public abstract void dropPrototype(String name);

   public abstract void createSchema();

   public abstract void updateSchema();

   public abstract void dropSchema();

   public abstract void persistObject(OpObject object);

   public abstract OpObject getObject(Class c, long id);

   public abstract void updateObject(OpObject object);

   public abstract void deleteObject(OpObject object);

   public abstract List list(OpQuery query);

   public abstract Iterator iterate(OpQuery query);

   public abstract int execute(OpQuery query);

   public abstract OpTransaction newTransaction();

   public abstract Blob newBlob(byte[] bytes);

   public abstract OpQuery newQuery(String s);

   public abstract void close(); // On order to free resources (e.g., JDBC-driver)

   public abstract Connection getJDBCConnection();

   public abstract boolean isValid();

   public abstract boolean isOpen();
}
