/*
 * Copyright(c) Onepoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence.hibernate;

import onepoint.persistence.OpObject;
import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Hibernate interceptor which sets the <code>Created</code> and <code>Modified</code>
 * fields on the newly instantiated/updated <code>OpObject</code> instances.
 *
 * @author horia.chiorean
 */
public final class OpTimestampInterceptor extends EmptyInterceptor {

   /**
    * @see org.hibernate.Interceptor#onFlushDirty(Object, java.io.Serializable, Object[], Object[], String[], org.hibernate.type.Type[])
    */
   @Override
   public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
      boolean modified = false;
      Timestamp currentGMT = getCurrentTimeGMT();
      for (int i = 0; i < propertyNames.length; i++) {
         if (propertyNames[i].equals(OpObject.MODIFIED)) {
            currentState[i] = currentGMT;
            modified = true;
            break;
         }
      }
      return modified;
   }

   /**
    * @see org.hibernate.Interceptor#onSave(Object, java.io.Serializable, Object[], String[], org.hibernate.type.Type[])
    */
   @Override
   public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
      boolean modified = false;
      Timestamp currentGMT = getCurrentTimeGMT();
      for (int i = 0; i < propertyNames.length; i++) {
         if (propertyNames[i].equals(OpObject.CREATED)) {
            if (state[i] == null) {
               state[i] = currentGMT;
               modified = true;
            }
            break;
         }
      }
      return modified;
   }

   /**
    * Returns a timestamp with the current time in GMT format.
    * @return a <code>Timestamp</code> with the current time in GMT format.
    */
   private Timestamp getCurrentTimeGMT() {
      TimeZone gmtTimezone = TimeZone.getTimeZone("GMT");
      //this is need because of the JDBC timezone shift
      if (!TimeZone.getDefault().equals(gmtTimezone)) {
         TimeZone.setDefault(gmtTimezone);
      }
      Calendar calendar = Calendar.getInstance(gmtTimezone);
      return new Timestamp(calendar.getTime().getTime());
   }
}
