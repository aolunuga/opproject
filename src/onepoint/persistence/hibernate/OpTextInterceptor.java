package onepoint.persistence.hibernate;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;

import java.io.Serializable;

/**
 * Hibernate interceptor which truncates the string fields to a fixed size.
 *
 * @author mihai.costin
 */
public class OpTextInterceptor extends EmptyInterceptor {

   XLog logger = XLogFactory.getServerLogger(OpTextInterceptor.class);
   private int maxLength;

   /**
    * @param length Maximum string data length.
    */
   public OpTextInterceptor(int length) {
      maxLength = length;
   }

   /**
    * @see org.hibernate.Interceptor#onSave(Object, java.io.Serializable, Object[], String[], org.hibernate.type.Type[])
    */
   @Override
   public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
      return truncateString(state, types);
   }

   /**
    * @see org.hibernate.Interceptor#onFlushDirty(Object, java.io.Serializable, Object[], Object[], String[], org.hibernate.type.Type[])
    */
   @Override
   public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
      return truncateString(currentState, types);
   }

   private boolean truncateString(Object[] state, Type[] types) {
      boolean modified = false;
      for (int i = 0; i < types.length; i++) {
         if (types[i].getReturnedClass() == String.class) {
            if (state[i] != null && ((String) state[i]).length() > maxLength) {
               logger.warn("Data truncation has occured (to" + maxLength + " chars) for " + state[i]);
               state[i] = ((String) state[i]).subSequence(0, maxLength);
               modified = true;
            }
         }
      }
      return modified;
   }
   
}
