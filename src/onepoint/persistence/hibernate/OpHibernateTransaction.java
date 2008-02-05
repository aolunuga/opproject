/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence.hibernate;

import onepoint.error.XLocalizableException;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpPersistenceException;
import onepoint.persistence.OpTransaction;
import onepoint.project.modules.user.OpUserError;
import onepoint.project.modules.user.OpUserService;
import onepoint.service.XError;
import onepoint.service.server.XServiceException;

import org.hibernate.HibernateException;
import org.hibernate.Transaction;

public final class OpHibernateTransaction implements OpTransaction {

   private static final XLog logger = XLogFactory.getServerLogger(OpHibernateTransaction.class);

   private Transaction transaction;

   OpHibernateTransaction(Transaction transaction) {
      this.transaction = transaction;
   }

   public void commit() {
      try {
         transaction.commit();
      }
      catch (HibernateException e) {
         logger.error("OpHibernateTransaction.commit(): Could not commit transaction: ", e);
         throw new OpPersistenceException(e);
      }
      catch (IllegalStateException e) {
         logger.error("OpHibernateTransaction.commit(): Could not commit transaction: ", e);
         throw new XLocalizableException(OpUserService.ERROR_MAP, OpUserError.SITE_IS_INVALID);
      }
      catch (RuntimeException e) {
         logger.error("OpHibernateTransaction.commit(): Could not commit transaction: ", e);
         throw new OpPersistenceException(e);
      }
   }

   public void rollback() {
      try {
         transaction.rollback();
      }
      catch (HibernateException e) {
         logger.error("OpHibernateTransaction.rollback(): Could not rollback transaction: " + e);
         throw new OpPersistenceException(e);
      }
   }

   public boolean wasCommited() {
      try {
         return transaction.wasCommitted();
      }
      catch (HibernateException e) {
         logger.error("OpHibernateTransaction.wasCommited(): Could not check transaction: " + e);
         throw new OpPersistenceException(e);
      }
   }

   public boolean isActive() {
      try {
         return transaction.isActive();
      }
      catch (HibernateException e) {
         logger.error("OpHibernateTransaction.isActive(): Could not check transaction: " + e);
         throw new OpPersistenceException(e);
      }
   }

   public void rollbackIfNecessary() {
      if (!wasCommited()) {
         rollback();
      }
   }


}
