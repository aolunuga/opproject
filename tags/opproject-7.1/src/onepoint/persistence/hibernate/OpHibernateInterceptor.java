/*
 * Copyright(c) Onepoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence.hibernate;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.CallbackException;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

/**
 * Adapter class, representing a Hibernate interceptor which in turn can contain multiple
 * interceptors. The ideea is to give each contained interceptor the chance to react to the
 * "events" given out by Hibernate.
 *
 * @author horia.chiorean
 */
public class OpHibernateInterceptor extends EmptyInterceptor {

   /**
    * The list of hibernate interceptors agregated by this interceptor.
    */
   private List<Interceptor> interceptors = null;


   /**
    * Creates a new instances, with the given list of hibernate interceptors.
    *
    * @param interceptors a <code>List(Interceptor)</code>.
    */
   OpHibernateInterceptor(List<Interceptor> interceptors) {
      this.interceptors = (interceptors != null) ? interceptors : new ArrayList<Interceptor>();
   }

   /**
    * @see org.hibernate.Interceptor#onLoad(Object, java.io.Serializable, Object[], String[], org.hibernate.type.Type[])
    */
   @Override
   public boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
      if (interceptors.isEmpty()) {
         return super.onLoad(entity, id, state, propertyNames, types);
      }
      boolean stateModified = false;
      for (Interceptor interceptor : interceptors) {
         stateModified |= interceptor.onLoad(entity, id, state, propertyNames, types);
      }
      return stateModified;
   }

   /**
    * @see org.hibernate.Interceptor#onSave(Object, java.io.Serializable, Object[], String[], org.hibernate.type.Type[])
    */
   @Override
   public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
      if (interceptors.isEmpty()) {
         return super.onSave(entity, id, state, propertyNames, types);
      }
      boolean stateModified = false;
      for (Interceptor interceptor : interceptors) {
         stateModified |= interceptor.onSave(entity, id, state, propertyNames, types);
      }
      return stateModified;
   }

   /**
    * @see org.hibernate.Interceptor#onPrepareStatement(String)
    */
   @Override
   public String onPrepareStatement(String sql) {
       if (interceptors.isEmpty()) {
         return super.onPrepareStatement(sql);
      }
      String preparedSql = null;
      for (Interceptor interceptor : interceptors) {
         if (preparedSql == null) {
            preparedSql = interceptor.onPrepareStatement(sql);
         }
         else {
            preparedSql = interceptor.onPrepareStatement(preparedSql);
         }
      }
      return preparedSql;
   }

   /**
    * @see Interceptor#onFlushDirty(Object, java.io.Serializable, Object[], Object[], String[], org.hibernate.type.Type[])
    */
   @Override
   public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
      if (interceptors.isEmpty()) {
         return super.onFlushDirty(entity, id, currentState, previousState, propertyNames,types);
      }
      boolean stateModified = false;
      for (Interceptor interceptor : interceptors) {
         stateModified |= interceptor.onFlushDirty(entity, id, currentState, previousState, propertyNames, types);
      }
      return stateModified;
   }

   /**
    * @see org.hibernate.Interceptor#afterTransactionBegin(org.hibernate.Transaction)
    */
   @Override
   public void afterTransactionBegin(Transaction transaction) {
      if (interceptors.isEmpty()) {
         super.afterTransactionBegin(transaction);
      }
      else {
         for (Interceptor interceptor : interceptors) {
            interceptor.afterTransactionBegin(transaction);
         }
      }
   }

   /**
    * @see org.hibernate.Interceptor#afterTransactionCompletion(org.hibernate.Transaction)
    */
   @Override
   public void afterTransactionCompletion(Transaction transaction) {
      if (interceptors.isEmpty()) {
         super.afterTransactionCompletion(transaction);
      }
      else {
         for (Interceptor interceptor : interceptors) {
            interceptor.afterTransactionCompletion(transaction);
         }
      }
   }

   /**
    * @see org.hibernate.Interceptor#beforeTransactionCompletion(org.hibernate.Transaction)
    */
   @Override
   public void beforeTransactionCompletion(Transaction transaction) {
      if (interceptors.isEmpty()) {
         super.beforeTransactionCompletion(transaction);
      }
      else {
         for (Interceptor interceptor : interceptors) {
            interceptor.beforeTransactionCompletion(transaction);
         }
      }
   }

   /**
    * @see org.hibernate.Interceptor#onCollectionRemove(Object, java.io.Serializable)
    */
   @Override
   public void onCollectionRemove(Object collection, Serializable key)
        throws CallbackException {
      if (interceptors.isEmpty()) {
         super.onCollectionRemove(collection, key);
      }
      else {
         for (Interceptor interceptor : interceptors) {
            interceptor.onCollectionRemove(collection, key);
         }
      }
   }

   /**
    * @see org.hibernate.Interceptor#onCollectionRecreate(Object, java.io.Serializable)
    */
   @Override
   public void onCollectionRecreate(Object collection, Serializable key)
        throws CallbackException {
      if (interceptors.isEmpty()) {
         super.onCollectionRecreate(collection, key);
      }
      else {
         for (Interceptor interceptor : interceptors) {
            interceptor.onCollectionRecreate(collection, key);
         }
      }
   }

   /**
    * @see org.hibernate.Interceptor#onCollectionUpdate(Object, java.io.Serializable)
    */
   @Override
   public void onCollectionUpdate(Object collection, Serializable key)
        throws CallbackException {
      if (interceptors.isEmpty()) {
         super.onCollectionUpdate(collection, key);
      }
      else {
         for (Interceptor interceptor : interceptors) {
            interceptor.onCollectionUpdate(collection, key);
         }
      }
   }

   /**
    * @see org.hibernate.Interceptor#postFlush(java.util.Iterator)
    */
   @Override
   public void postFlush(Iterator entities) {
      if (interceptors.isEmpty()) {
         super.postFlush(entities);
      }
      else {
         for (Interceptor interceptor : interceptors) {
            interceptor.postFlush(entities);
         }
      }
   }

   /**
    * @see org.hibernate.Interceptor#preFlush(java.util.Iterator)
    */
   @Override
   public void preFlush(Iterator entities) {
      if (interceptors.isEmpty()) {
         super.preFlush(entities);
      }
      else {
         for (Interceptor interceptor : interceptors) {
            interceptor.preFlush(entities);
         }
      }
   }

   /**
    * @see org.hibernate.Interceptor#onDelete(Object, java.io.Serializable, Object[], String[], org.hibernate.type.Type[])
    */
   @Override
   public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
      if (interceptors.isEmpty()) {
         super.onDelete(entity, id, state, propertyNames, types);
      }
      else {
         for (Interceptor interceptor : interceptors) {
            interceptor.onDelete(entity, id, state, propertyNames, types);
         }
      }
   }
}
