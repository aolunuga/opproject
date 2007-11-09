/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */ 

/**
 * 
 */
package onepoint.persistence.hibernate;

import java.lang.reflect.Field;

import org.hibernate.PropertyNotFoundException;
import org.hibernate.property.BasicPropertyAccessor;
import org.hibernate.property.DirectPropertyAccessor;
import org.hibernate.property.Getter;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.Setter;

/**
 * @author dfreis
 *
 */
public class OpPropertyAccessor implements PropertyAccessor {
   private final static PropertyAccessor FIELD_ACCESSOR = new DirectPropertyAccessor();
   private final static PropertyAccessor METHOD_ACCESSOR = new BasicPropertyAccessor();
   
   private final static String INTERNAL = "Internal"; 
   /* (non-Javadoc)
    * @see org.hibernate.property.PropertyAccessor#getGetter(java.lang.Class, java.lang.String)
    */
   public Getter getGetter(Class theClass, String propertyName)
         throws PropertyNotFoundException {
      try {
         return METHOD_ACCESSOR.getGetter(theClass, propertyName+INTERNAL);
      }
       catch (PropertyNotFoundException exc) {
          return FIELD_ACCESSOR.getGetter(theClass, getFieldName(propertyName));
       }
   }

   /* (non-Javadoc)
    * @see org.hibernate.property.PropertyAccessor#getSetter(java.lang.Class, java.lang.String)
    */
   public Setter getSetter(Class theClass, String propertyName)
         throws PropertyNotFoundException {
      try {
         return METHOD_ACCESSOR.getSetter(theClass, propertyName+INTERNAL);
      }
      catch (PropertyNotFoundException exc) {
         return FIELD_ACCESSOR.getSetter(theClass, getFieldName(propertyName));
      }
   }
   
   private String getFieldName(String propertyName) {
      String ret = propertyName.substring(0,1).toLowerCase()+propertyName.substring(1);
      if (ret.equals("iD")) {
         ret = "id";
      }
      return ret;
   }
}
