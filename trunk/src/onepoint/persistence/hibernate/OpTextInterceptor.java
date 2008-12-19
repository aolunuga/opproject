package onepoint.persistence.hibernate;

import java.io.Serializable;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpMember;
import onepoint.persistence.OpObjectIfc;
import onepoint.persistence.OpPrototype;
import onepoint.persistence.OpType;
import onepoint.persistence.OpTypeManager;

import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;

/**
 * Hibernate interceptor which truncates the string fields to a fixed size.
 *
 * @author mihai.costin
 */
public class OpTextInterceptor extends EmptyInterceptor {

   /**
    * This class's logger
    */
   private static final XLog logger = XLogFactory.getLogger(OpTextInterceptor.class);


   /**
    * @see org.hibernate.Interceptor#onSave(Object, java.io.Serializable, Object[], String[], org.hibernate.type.Type[])
    */
   @Override
   public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
      return truncateString(entity, state, propertyNames);
   }

   /**
    * @see org.hibernate.Interceptor#onFlushDirty(Object, java.io.Serializable, Object[], Object[], String[], org.hibernate.type.Type[])
    */
   @Override
   public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
      return truncateString(entity, currentState, propertyNames);
   }

   /**
    * Truncates the string  properties of the object to make sure it fits in the database.
    * @param entity an <code>Object</code> representing a hibernate entity.
    * @param state a <code>Object[]</code> the current values of the properties of the object.
    * @param propertyNames a <code>String[]</code> the property names of the object.
    * @return <code>true</code> if the object was modified, false otherwise.
    */
   private boolean truncateString(Object entity, Object[] state, String[] propertyNames) {
      boolean modified = false;
      OpObjectIfc object = (OpObjectIfc) entity;
      OpPrototype prototype = OpTypeManager.getPrototypeForObject(object);
      for (int i = 0; i < propertyNames.length; i++) {
         String property = propertyNames[i];
         Object value = state[i];
         //for performance reasons only
         if (!(value instanceof  String) || ((String) value).length() <= OpTypeManager.MAX_STRING_LENGTH) {
            continue;
         }
         OpMember member = prototype.getMember(property);
         switch (member.getTypeID()) {
            case OpType.STRING: {
               String valueString = (String) value;
               if (valueString.length() > OpTypeManager.MAX_STRING_LENGTH) {
                  logger.warn("Property " + property + " of object " + prototype.getName() + " is too long. Truncating to " + OpTypeManager.MAX_STRING_LENGTH);
                  state[i] = valueString.substring(0, OpTypeManager.MAX_STRING_LENGTH);
                  modified = true;
               }
               break;
            }
            case OpType.TEXT: {
               String valueString = (String) value;
               if (valueString.length() > OpTypeManager.MAX_TEXT_LENGTH) {
                  logger.warn("Property " + property + " of object " + prototype.getName() + " is too long. Truncating to " + OpTypeManager.MAX_TEXT_LENGTH);
                  state[i] = valueString.substring(0, OpTypeManager.MAX_TEXT_LENGTH);
                  modified = true;
               }
               break;
            }
         }
      }
      return modified;
   }

}
