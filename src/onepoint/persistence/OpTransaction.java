/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

public interface OpTransaction {

   public void commit();

   public void rollback();

   public boolean wasCommited();

   public boolean isActive();

   /**
    * Rolls back the transaction if it wasn't commited.
    */
   public void rollbackIfNecessary();
}

