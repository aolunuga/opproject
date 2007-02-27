/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence.hibernate;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpTransaction;
import org.hibernate.HibernateException;
import org.hibernate.Transaction;

public final class OpHibernateTransaction implements OpTransaction {
   
   private static final XLog logger = XLogFactory.getLogger(OpHibernateTransaction.class,true);

	private Transaction _transaction;

	OpHibernateTransaction(Transaction transaction) {
		_transaction = transaction;
	}

	public final void commit() {
		try {
			_transaction.commit();
		}
		catch (HibernateException e) {
			logger.error("OpHibernateTransaction.commit(): Could not commit transaction: ", e);
			// *** TODO: Throw OpPersistenceException
		}
	}

	public final void rollback() {
		try {
			_transaction.rollback();
		}
		catch (HibernateException e) {
			logger.error("OpHibernateTransaction.rollback(): Could not rollback transaction: " + e);
			// *** TODO: Throw OpPersistenceException
		}
	}

}
